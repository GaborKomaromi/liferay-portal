/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.batch.engine.internal;

import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.util.CSVUtil;

/**
 * Neutralizes spreadsheet formula injection (CWE-1236) when exporting to CSV
 * and XLS, and reverses the neutralization when importing so that a value
 * survives an export and import round trip unchanged. The neutralization is
 * shared with the rest of the portal through {@link CSVUtil}; only the inverse,
 * needed because the batch engine re-imports its own exports, lives here.
 *
 * @author Gabor Komaromi
 */
public class BatchEngineFormulaInjectionUtil {

	public static Object neutralize(Object value) {
		if (!(value instanceof String)) {
			return value;
		}

		return CSVUtil.escapeValue((String)value);
	}

	public static String restore(String value) {
		if ((value == null) || (value.length() < 2) ||
			(value.charAt(0) != CharPool.APOSTROPHE) ||
			!CSVUtil.isFormulaInjectionPrefix(value.charAt(1))) {

			return value;
		}

		return value.substring(1);
	}

}
