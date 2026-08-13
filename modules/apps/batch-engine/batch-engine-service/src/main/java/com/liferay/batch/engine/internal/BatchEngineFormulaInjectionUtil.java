/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.batch.engine.internal;

import com.liferay.portal.kernel.util.CSVUtil;

/**
 * Neutralizes spreadsheet formula injection (CWE-1236) when exporting to CSV
 * and XLS. The neutralization logic is shared with the rest of the portal
 * through {@link CSVUtil}.
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

}
