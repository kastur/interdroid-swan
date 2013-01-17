package interdroid.swan.sensors;

public class DataRequest implements Comparable<DataRequest> {
	private long start;
	private long end;
	
	public long getStart() {
		return start;
	}
	public void setStart(long start) {
		this.start = start;
	}
	public long getEnd() {
		return end;
	}
	public void setEnd(long end) {
		this.end = end;
	}
	
	public DataRequest(long start, long end) {
		super();
		this.start = start;
		this.end = end;
	}
	
	@Override
	public int compareTo(DataRequest another) {
		return (int)(this.start - another.start);
	}
}