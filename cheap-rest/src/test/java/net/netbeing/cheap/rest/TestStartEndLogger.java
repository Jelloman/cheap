/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.rest;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestStartEndLogger implements BeforeTestExecutionCallback, AfterTestExecutionCallback
{
    private static final Logger logger = LoggerFactory.getLogger(TestStartEndLogger.class);

    @Override
    public void beforeTestExecution(ExtensionContext context)
    {
        logger.info("Start test {}", context.getDisplayName());
    }

    @Override
    public void afterTestExecution(ExtensionContext context)
    {
        logger.info("End test {}", context.getDisplayName());
    }
}