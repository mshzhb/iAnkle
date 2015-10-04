package edu.utoronto.cimsah.myankle;

import android.util.Log;

import junit.framework.TestCase;

import java.util.ArrayList;

import edu.utoronto.cimsah.myankle.Helpers.Samples;

/**
 * Created by Edwin on 05/08/2015.
 */
public class SamplesTest extends TestCase {
    final float GRAVITY = Samples.GRAVITY;
    final float TOLERANCE = 0.0001f;
    static String TAG = SamplesTest.class.getSimpleName();

    ArrayList<Float> NOCAL = new ArrayList<>(6);
    ArrayList<Float> TESTCAL = new ArrayList<>(6);
    ArrayList<Float> ZEROCAL = new ArrayList<>(6);


    @Override
    public void setUp() throws Exception {
        super.setUp();

        //Sets all cal points to g
        //This means accelerometers are perfect, hence no calibration is needed when sampling
        for (int x = 0; x < 6; x++) {
            NOCAL.add(GRAVITY);
            ZEROCAL.add(0f);
        }

        //Dummy calibration values chosen to be near 9.81N
        //Calibration axes are +X/+Y/+Z/-X/-Y/-Z
        TESTCAL.add(9.5f);
        TESTCAL.add(10.0f);
        TESTCAL.add(9.7f);
        TESTCAL.add(9.9f);
        TESTCAL.add(9.1f);
        TESTCAL.add(10.1f);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    //Tests getNumSamples function in Samples class
    public final void testGetNumSamples() {
        Samples samples = new Samples(NOCAL);
        assertEquals(0, samples.getNumSamples());

        for (float f = 1; f <= 5; f++) {
            samples.add(f, f, f, f);
            assertEquals((int) f, samples.getNumSamples());
        }

        samples.clear();
        assertEquals(0, samples.getNumSamples());
        samples = null;
    }

    public final void testVectorMagnitude() {
        Samples samples = new Samples(NOCAL);
        assertEquals(GRAVITY, samples.vectorMagnitude(GRAVITY, 0, 0, 0, 0, 0, 0));
        assertEquals(0.0f, samples.vectorMagnitude());
        assertEquals(0.0f, samples.vectorMagnitude(0, 0, 0, 0));
        assertEquals(156.46518f, samples.vectorMagnitude(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 155.23f));

        samples = null;
    }

    public final void testComputeGFactor() {
        Samples samples = new Samples(NOCAL);
        assertEquals(2.4849863f, samples.computeGFactor(1.2f, 2.1f, 3.12f));
        samples = null;
    }

    public final void testSampleInstability() {
        Samples samples = new Samples(NOCAL);
        samples.add(0f, 3.5f, 2.4f, 4.7f);
        Samples.Sample sample = samples.getSample(0); //retrieves the newsly created sample

        //Test calculation of correction factors
        float factors[] = samples.computeCorrectionFactors(1.2f, 2.1f, 3.12f);
        assertEquals(2.9819837f, factors[0]);
        assertEquals(5.218471f, factors[1]);
        assertEquals(7.753157f, factors[2]);

        //Test calculation of sample instability
        assertEquals(4.1873484f, samples.getSampleInstability(sample,factors));

        samples = null;
    }

    //Tests getSample function in Samples class
    public final void testAddAndGetSample() {
        Samples samples = new Samples(NOCAL);
        Samples.Sample sample;

        float testT = 1.4f;
        float testX = 2.5f;
        float testY = 3.9f;
        float testZ = 4.2f;

        assertEquals(0, samples.getNumSamples());

        //Test with dummy data point
        samples.add(testT, testX, testY, testZ);
        assertEquals(1, samples.getNumSamples());

        //Retrieve data
        sample = samples.getSample(0);
        assertEquals(testT, sample.t(), TOLERANCE);
        assertEquals(testX, sample.x_cal(), TOLERANCE);
        assertEquals(testY, sample.y_cal(), TOLERANCE);
        assertEquals(testZ, sample.z_cal(), TOLERANCE);

        samples.clear();
        samples = null;
    }

    //Tests clear function from the Samples class
    public final void testClear() {
        Samples samples = new Samples(NOCAL);

        checkSamplesZeroed(samples); //check initial conditions

        addRandomSamplePoints(samples);

        assert (samples.getNumSamples() > 0);
        assert (samples.getxSum() > 0);
        assert (samples.getySum() > 0);
        assert (samples.getzSum() > 0);

        samples.clear();
        checkSamplesZeroed(samples);
    }

    //Checks all values of a given sample is zeroed
    void checkSamplesZeroed(Samples samples) {
        assertEquals(0, samples.getNumSamples());
        assertEquals(0.0f, samples.getxSum());
        assertEquals(0.0f, samples.getySum());
        assertEquals(0.0f, samples.getzSum());
    }

    public final void testInvalidInput() {

        Samples samples = new Samples(ZEROCAL);

        //Calculating balance number without any samples registered should return 0
        assertEquals(0f, samples.get_mean_r(), TOLERANCE);

        //Calibration of <0,0,0,0,0,0> will be treated as <g,g,g,g,g,g>
        //This will result in calibration having no effect, instead dividing by zero
        samples.add(0f, GRAVITY, 0, 0);
        assertEquals(0f, samples.get_mean_r(), TOLERANCE);
        samples.clear();

        //A null/zero sample shouldn't occur and will be refused
        samples.add(1f, 0f, 0f, 0f);
        assertEquals(0, samples.getNumSamples());
    }

    //Tests phone resting perfectly still on each side, with no calibration
    public final void testStillPhoneNoCal() {
        Samples samples = new Samples(NOCAL);

        //Test resting on side (X Axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, GRAVITY, 0, 0);
        assertEquals(0f, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test resting on side (-X Axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, -GRAVITY, 0, 0);
        assertEquals(0f, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test resting flat on table (Z axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, 0, 0, GRAVITY);
        assertEquals(0f, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test resting face down on table (Z axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, 0, 0, -GRAVITY);
        assertEquals(0f, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test Resting on side (Y Axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, 0, GRAVITY, 0);
        assertEquals(0f, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test Resting on side (-Y Axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, 0, -GRAVITY, 0);
        assertEquals(0f, samples.get_mean_r(), TOLERANCE);

        samples.clear();
        samples = null;
    }

    //Tests phone resting perfectly still on each side, with calibration
    //Results will all be different due to the differing calibration values on each +/- axis
    public final void testStillPhoneWithCal() {
        Samples samples = new Samples(TESTCAL);

        //Test resting on side (X Axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, GRAVITY, 0, 0);
        assertEquals(0.32011604f, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test resting on side (-X Axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, -GRAVITY, 0, 0);
        assertEquals(0.08918095f, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test resting flat on table (Z axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, 0, 0, GRAVITY);
        assertEquals(0.11124802, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test resting face down on table (Z axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, 0, 0, -GRAVITY);
        assertEquals(0.28167343, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test Resting on side (Y Axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, 0, GRAVITY, 0);
        assertEquals(0.18638897, samples.get_mean_r(), TOLERANCE);

        samples.clear();

        //Test Resting on side (-Y Axis)
        assertEquals(0, samples.getNumSamples());
        samples.add(0, 0, -GRAVITY, 0);
        assertEquals(0.76539516, samples.get_mean_r(), TOLERANCE);

        samples.clear();
        samples = null;
    }

    //Tests class using many samples simulating movement and a calibration
    public final void testMovePhoneWithCal() {
        Samples samples = new Samples(TESTCAL);
        assertEquals(0, samples.getNumSamples());

        addRandomSamplePoints(samples);

        assertEquals(500, samples.getNumSamples());
        assertEquals(121.43753f, samples.get_mean_r(), TOLERANCE);

        samples.clear();
        samples = null;
    }

    //Uses typical acceleration data without calibration
    public final void testRepresentativeDataWithoutCal() {
        Samples samples = new Samples(NOCAL);
        assertEquals(0, samples.getNumSamples());

        loadCsvDataFromString(samples, Data.representativeData);
        assertEquals(0.30417526, samples.get_mean_r(), TOLERANCE);

        samples.clear();
        samples = null;
    }

    //Uses typical acceleration data with a calibration
    public final void testRepresentativeDataWithCal() {
        Samples samples = new Samples(TESTCAL);
        assertEquals(0, samples.getNumSamples());

        loadCsvDataFromString(samples, Data.representativeData);

        assertEquals(0.2652227f, samples.get_mean_r(), TOLERANCE);

        samples.clear();
        samples = null;
    }

    //Clears samples and adds 500 new sample points (not representative of typical use)
    void addRandomSamplePoints(Samples s) {
        s.clear();

        //Generate and add 500 dummy data points
        for (float f = 1; f <= 500; f++) {
            float time = f / 100.0f;
            float xAccel = (float) Math.pow(-1, f) * (f % 3) / 123.4f;
            float yAccel = 643 % f + f / 251.2f;
            float zAccel = (float) Math.pow(-1, f) * 621.25f / f + f % 2;

            s.add(time, xAccel, yAccel, zAccel);
        }

        return;
    }

    /**
     * Loads CSV String of data into a Samples
     *
     * @param s    Sample to load data into
     * @param data String of CSV data (stored in src/androidTest/java/edu.utoronto.cimsah.myankle/Data.java)
     */
    void loadCsvDataFromString(Samples s, String data) {
        String lines[] = data.split("\n"); //Separate individual lines of data

        //Iterate through each line of data, extract individual data from each line
        for (String line : lines) {
            String[] RowData = line.split(",");

            //Expect each line to contain: time, x, y, z data (in that order)
            if (RowData.length == 4) {
                float time = Float.parseFloat(RowData[0]);
                float xAccel = Float.parseFloat(RowData[1]);
                float yAccel = Float.parseFloat(RowData[2]);
                float zAccel = Float.parseFloat(RowData[3]);

                s.add(time, xAccel, yAccel, zAccel);
            } else Log.e(TAG, "Invalid CSV data - lines must of of format 'time','x','y','z'");
        }
    }

}
