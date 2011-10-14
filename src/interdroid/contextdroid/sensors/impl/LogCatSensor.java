package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.sensors.AbstractMemorySensor;
import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;

public class LogCatSensor extends AbstractMemorySensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(LogCatSensor.class);

	public static final String LOG_FIELD = "log";

	public static final String LOGCAT_PARAMETERS = "logcat_parameters";

	public static final String DEFAULT_LOGCAT_PARAMETERS = "*:I";

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 5 * 60 * 1000;

	private Map<String, LogcatPoller> activeThreads = new HashMap<String, LogcatPoller>();

	@Override
	public String[] getValuePaths() {
		return new String[] { LOG_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putString(LOGCAT_PARAMETERS,
				DEFAULT_LOGCAT_PARAMETERS);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'train', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ LOG_FIELD
				+ "', 'type': 'string'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	public final void register(String id, String valuePath,
			Bundle configuration) {
		LOG.debug("Logcat got registration for: {}", id);
		LogcatPoller logcatPoller = new LogcatPoller(id, configuration);
		activeThreads.put(id, logcatPoller);
		logcatPoller.start();
	}

	@Override
	public final void unregister(String id) {
		activeThreads.remove(id).terminate();
	}

	class LogcatPoller extends Thread {

		private Bundle configuration;
		private List<TimestampedValue> values = new ArrayList<TimestampedValue>();
		private String id;
		private Process process;

		LogcatPoller(String id, Bundle configuration) {
			this.configuration = configuration;
			this.id = id;
		}

		public void terminate() {
			process.destroy();
		}

		public void run() {
			new Thread() {
				public void run() {
					String parameters = configuration
							.getString(LOGCAT_PARAMETERS);
					if (parameters == null) {
						parameters = mDefaultConfiguration
								.getString(LOGCAT_PARAMETERS);
					}
					try {
						process = Runtime.getRuntime().exec(
								"logcat " + parameters);
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(process.getInputStream()));
						String line = null;
						while ((line = reader.readLine()) != null) {
							long now = System.currentTimeMillis();
							if (values.size() >= HISTORY_SIZE) {
								values.remove(0);
							}
							values.add(new TimestampedValue(line, now, now
									+ EXPIRE_TIME));
							notifyDataChangedForId(id);
						}
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();

		}

		public List<TimestampedValue> getValues() {
			return values;
		}
	}

	@Override
	public void onDestroySensor() {
		for (LogcatPoller logcatPoller : activeThreads.values()) {
			logcatPoller.interrupt();
		}
	};

}
