/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as fs from 'fs';

import {HeadlessBatchEngineClient} from "../../../apps/headless/headless-batch-engine/headless-batch-engine-client-js/src/main/resources/META-INF/resources/node"
import getRandomString from '../utils/getRandomString';
import {ApiHelpers, DataApiHelpers} from './ApiHelpers';

type TExportTask = {
	className?: string;
	contentType?: string;
	errorMessage?: string;
	executeStatus?: string;
	externalReferenceCode?: string;
	id?: number;
	processedItemsCount?: number;
	startTime?: string;
	totalItemsCount?: number;
};

export class HeadlessBatchEngineApiHelper {
	readonly apiHelpers: ApiHelpers | DataApiHelpers;
	readonly basePath: string;

	constructor(apiHelpers: ApiHelpers) {
		this.apiHelpers = apiHelpers;
		this.basePath = 'headless-batch-engine/v1.0/';
	}

	async getExportTask(exportTaskId: number): Promise<TExportTask> {

		const headlessBatchEngineClient = await this.apiHelpers.buildRestClient(
			HeadlessBatchEngineClient
		);

		return headlessBatchEngineClient.exportTask.getExportTask({exportTaskId: exportTaskId})

	}

	async getExportTaskContent(exportTaskId: number) {
		
		
		const headlessBatchEngineClient = await this.apiHelpers.buildRestClient(
			HeadlessBatchEngineClient
		);

		return headlessBatchEngineClient.exportTask.getExportTaskContent({exportTaskId: exportTaskId})
	}
}
