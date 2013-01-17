package interdroid.swan.contextexpressions;

import android.os.Parcel;
import android.os.Parcelable;
import interdroid.swan.SwanException;
import interdroid.swan.ContextManager;
import interdroid.swan.contextservice.SensorConfigurationException;
import interdroid.swan.contextservice.SensorManager;
import interdroid.swan.contextservice.SensorSetupFailedException;

/**
 * An expression which compares two values.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * @author kasturi <kr@cs.ucla.edu>
 * 
 */

public class ConditionalExpression extends Expression {

	private static final String TAG = "ConditionalExpression";

	private static final long SLEEP_THRESHOLD = 60 * 1000; //

	private static final long serialVersionUID = 4074214646065165493L;
	
	private final Expression mConditionExpression;

	private final Expression mTrueExpression;
	
	private final Expression mFalseExpression;
	
	private Expression mActiveExpression = null;

	/**
	 * The time until this value expression needs to be evaluated again.
	 */
	private long mDeferUntil = -1;

	/**
	 * A reference to the SensorManager, is needed to implement
	 * sleep-and-be-ready method
	 */
	private SensorManager mSensorManager;


	public ConditionalExpression(final Expression condition,
			final Expression trueExpression, final Expression falseExpression) {
		this.mConditionExpression = condition;
		this.mTrueExpression = trueExpression;
		this.mFalseExpression = falseExpression;
		this.mActiveExpression = null;
	}


	protected ConditionalExpression(final Parcel in) {
		super(in);
		mConditionExpression = in.readParcelable(getClass().getClassLoader());
		mTrueExpression = in.readParcelable(getClass().getClassLoader());
		mFalseExpression = in.readParcelable(getClass().getClassLoader());
	}

	public static final Parcelable.Creator<ConditionalExpression> CREATOR
	= new Parcelable.Creator<ConditionalExpression>() {
		@Override
		public ConditionalExpression createFromParcel(final Parcel in) {
			return new ConditionalExpression(in);
		}

		@Override
		public ConditionalExpression[] newArray(final int size) {
			return new ConditionalExpression[size];
		}
	};

	@Override
	public final void initialize(final String id, final SensorManager sensorManager)
			throws SensorConfigurationException, SensorSetupFailedException {
		setId(id);
		mSensorManager = sensorManager;
		mConditionExpression.initialize(id + ".ConditionExpr", sensorManager);
		mTrueExpression.initialize(id + ".TrueExpr", sensorManager);
		mFalseExpression.initialize(id + ".FalseExpr", sensorManager);
		mActiveExpression = null;
	}

	@Override
	public final void destroy(final String id, final SensorManager sensorManager)
			throws SwanException {
		mConditionExpression.destroy(id + ".ConditionExpr", sensorManager);
		mTrueExpression.destroy(id + ".TrueExpr", sensorManager);
		mFalseExpression.destroy(id + ".FalseExpr", sensorManager);
	}

	@Override
	protected final void evaluateImpl(final long now)
			throws SwanException {

		mConditionExpression.evaluate();
		final int condResult = mConditionExpression.getResult();
		
		if (condResult == ContextManager.TRUE) {
			if (mTrueExpression.evaluate())  // true if the result of the expression changed.
				setResult(mTrueExpression.getResult(), mTrueExpression.getLastEvaluationTime());
			mActiveExpression = mTrueExpression;
			mDeferUntil = Math.min(mConditionExpression.getDeferUntil(), mTrueExpression.getDeferUntil());
		} else if (condResult == ContextManager.FALSE) {
			if (mFalseExpression.evaluate())
				setResult(mFalseExpression.getResult(), mFalseExpression.getLastEvaluationTime());
			mActiveExpression = mFalseExpression;
			mDeferUntil = Math.min(mConditionExpression.getDeferUntil(), mFalseExpression.getDeferUntil());
		} else {
			setResult(ContextManager.UNDEFINED, now);
			mActiveExpression = null;
			mDeferUntil = mConditionExpression.getDeferUntil();
		}
	}

	@Override
	protected final String toStringImpl() {
		return "if " + mConditionExpression + " then "
				+ mTrueExpression
				+ " else " + mFalseExpression;
	}

	@Override
	protected final String toParseStringImpl() {
		return "if " + mConditionExpression.toParseString() + " then "
				+ mTrueExpression.toParseString()
				+ " else " + mFalseExpression.toParseString();
	}

	@Override
	protected final void writeToParcelImpl(final Parcel dest, final int flags) {
		dest.writeParcelable(mConditionExpression, flags);
		dest.writeParcelable(mTrueExpression, flags);
		dest.writeParcelable(mFalseExpression, flags);
	}

	@Override
	protected final long getDeferUntilImpl() {
		return mDeferUntil;
	}

	@Override
	protected final int getSubtypeId() {
		return CONDITION_EXPRESSION_TYPE;
	}

	@Override
	public void sleepAndBeReadyAt(final long readyTime) {
	}

	@Override
	public long getHistoryLength() {
		// FIXME: What is the "history length" of a conditional expression?
		return Math.max(mTrueExpression.getHistoryLength(), mTrueExpression.getHistoryLength());
	}

	protected boolean hasCurrentTime() {
		return false;
	}

	@Override
	public TimestampedValue[] getValues(String string, long now) throws SwanException {
		if (mActiveExpression != null)
			return mActiveExpression.getValues(mActiveExpression.getId(), now);
		else
			return new TimestampedValue[] {
					new TimestampedValue(getResult(), getLastEvaluationTime()) };
	}

	@Override
	public HistoryReductionMode getHistoryReductionMode() {
		return HistoryReductionMode.DEFAULT_MODE;
	}

	@Override
	public boolean isConstant() {
		return 	mConditionExpression.isConstant() &&
				mTrueExpression.isConstant() &&
				mFalseExpression.isConstant();
	}

}
