/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.internal.model.listener;

import com.liferay.batch.engine.thread.local.BatchEngineThreadLocal;
import com.liferay.exportimport.kernel.lar.ExportImportThreadLocal;
import com.liferay.friendly.url.model.FriendlyURLEntry;
import com.liferay.friendly.url.model.FriendlyURLEntryLocalization;
import com.liferay.friendly.url.service.FriendlyURLEntryLocalService;
import com.liferay.layout.friendly.url.LayoutFriendlyURLEntryHelper;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.LayoutFriendlyURL;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.service.LayoutFriendlyURLLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.staging.StagingGroupHelper;

import java.util.Collections;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alejandro Tardín
 */
@Component(service = ModelListener.class)
public class LayoutFriendlyURLModelListener
	extends BaseModelListener<LayoutFriendlyURL> {

	@Override
	public void onAfterCreate(LayoutFriendlyURL layoutFriendlyURL)
		throws ModelListenerException {

		_addFriendlyURLEntry(layoutFriendlyURL);
	}

	@Override
	public void onAfterUpdate(
			LayoutFriendlyURL originalLayoutFriendlyURL,
			LayoutFriendlyURL layoutFriendlyURL)
		throws ModelListenerException {

		_addFriendlyURLEntry(layoutFriendlyURL);
	}

	private void _addFriendlyURLEntry(LayoutFriendlyURL layoutFriendlyURL) {
		if (!BatchEngineThreadLocal.isBatchImportInProcess() &&
			(ExportImportThreadLocal.isImportInProcess() ||
			 _stagingGroupHelper.isLiveGroup(layoutFriendlyURL.getGroupId()))) {

			return;
		}

		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();
		String uuid = null;

		if (serviceContext == null) {
			serviceContext = new ServiceContext();
		}
		else {
			uuid = serviceContext.getUuid();

			serviceContext.setUuid(null);
		}

		try {
			long classNameId = _layoutFriendlyURLEntryHelper.getClassNameId(
				layoutFriendlyURL.isPrivateLayout());

			String urlTitle = layoutFriendlyURL.getFriendlyURL();

			String uniqueUrlTitle = _toUniqueUrlTitle(
				layoutFriendlyURL, classNameId, urlTitle);

			_friendlyURLEntryLocalService.addFriendlyURLEntry(
				layoutFriendlyURL.getGroupId(), classNameId,
				layoutFriendlyURL.getPlid(),
				Collections.singletonMap(
					layoutFriendlyURL.getLanguageId(), uniqueUrlTitle),
				serviceContext);

			if (!uniqueUrlTitle.equals(urlTitle) &&
				!Boolean.TRUE.equals(_suppressUniqueResolution.get())) {

				_suppressUniqueResolution.set(Boolean.TRUE);

				try {
					layoutFriendlyURL.setFriendlyURL(uniqueUrlTitle);

					_layoutFriendlyURLLocalService.updateLayoutFriendlyURL(
						layoutFriendlyURL);
				}
				finally {
					_suppressUniqueResolution.remove();
				}
			}
		}
		catch (PortalException portalException) {
			throw new ModelListenerException(portalException);
		}
		finally {
			if (serviceContext != null) {
				serviceContext.setUuid(uuid);
			}
		}
	}

	private String _toUniqueUrlTitle(
		LayoutFriendlyURL layoutFriendlyURL, long classNameId,
		String urlTitle) {

		// Skip the lookup on the recursive update triggered by our own
		// LayoutFriendlyURL persistence below.

		if (Boolean.TRUE.equals(_suppressUniqueResolution.get())) {
			return urlTitle;
		}

		String uniqueUrlTitle = urlTitle;

		for (int i = 1;; i++) {
			FriendlyURLEntryLocalization friendlyURLEntryLocalization =
				_friendlyURLEntryLocalService.fetchFriendlyURLEntryLocalization(
					layoutFriendlyURL.getGroupId(), classNameId,
					uniqueUrlTitle);

			if ((friendlyURLEntryLocalization == null) ||
				(friendlyURLEntryLocalization.getClassPK() ==
					layoutFriendlyURL.getPlid())) {

				return uniqueUrlTitle;
			}

			// A localization whose owning FriendlyURLEntry is no longer the
			// main entry of its layout is a stale redirect; release it so the
			// current layout can claim the URL.

			FriendlyURLEntry currentFriendlyURLEntry =
				_friendlyURLEntryLocalService.fetchMainFriendlyURLEntry(
					classNameId, friendlyURLEntryLocalization.getClassPK());

			if ((currentFriendlyURLEntry == null) ||
				(currentFriendlyURLEntry.getFriendlyURLEntryId() !=
					friendlyURLEntryLocalization.getFriendlyURLEntryId())) {

				_friendlyURLEntryLocalService.
					deleteFriendlyURLLocalizationEntry(
						friendlyURLEntryLocalization.getFriendlyURLEntryId(),
						friendlyURLEntryLocalization.getLanguageId());

				return uniqueUrlTitle;
			}

			uniqueUrlTitle = urlTitle + i;
		}
	}

	private static final ThreadLocal<Boolean> _suppressUniqueResolution =
		new ThreadLocal<>();

	@Reference
	private FriendlyURLEntryLocalService _friendlyURLEntryLocalService;

	@Reference
	private LayoutFriendlyURLEntryHelper _layoutFriendlyURLEntryHelper;

	@Reference
	private LayoutFriendlyURLLocalService _layoutFriendlyURLLocalService;

	@Reference
	private StagingGroupHelper _stagingGroupHelper;

}