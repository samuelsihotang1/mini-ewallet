package io.github.samuelsihotang1.ewallet.e2e;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runs every feature under {@code features/} against the stack started by
 * {@code docker compose up -d --build --wait}. Base URLs come from the system properties
 * {@code e2e.wallet-url}, {@code e2e.transfer-url} and {@code e2e.notification-url}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.github.samuelsihotang1.ewallet.e2e")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
class TransferFeaturesIT {}
