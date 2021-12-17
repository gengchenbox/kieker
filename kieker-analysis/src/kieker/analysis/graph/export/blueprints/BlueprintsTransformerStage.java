/***************************************************************************
 * Copyright 2021 Kieker Project (http://kieker-monitoring.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package kieker.analysis.graph.export.blueprints;

import kieker.analysis.graph.IGraph;

import teetime.stage.basic.AbstractTransformation;

/**
 * @author Sören Henning
 *
 * @since 1.14
 */
public class BlueprintsTransformerStage extends AbstractTransformation<IGraph, com.tinkerpop.blueprints.Graph> {

	public BlueprintsTransformerStage() {
		super();
	}

	@Override
	protected void execute(final IGraph graph) {
		final BlueprintsTransformer transformer = new BlueprintsTransformer(graph);
		this.getOutputPort().send(transformer.transform());
	}

}
