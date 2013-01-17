package interdroid.swan.sensors;

import interdroid.swan.contextexpressions.TimestampedValue;

interface IAsynchronousContextSensor {

	void register(in String id, in String valuePath, in Bundle configuration);

	void unregister(in String id);

	List<TimestampedValue> getValues(in String id, long now, long timespan);
	
	void sendPullRequest(in String id, long start, long period, long windowSize, long nextDL);

	String getScheme();
}

