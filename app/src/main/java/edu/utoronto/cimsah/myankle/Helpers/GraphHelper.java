package edu.utoronto.cimsah.myankle.Helpers;

public class GraphHelper {
	
	/* Used to determine (heuristically) the optimal upper bound of the graph.
	 * the method is used to account for the chart values (point labels), which
	 * may be otherwise cut-off. to allow the upper bound to be set automatically,
	 * remove all instances of the method call */
	public static double estimateUpperBound(double[] values) {
		
		/* The margin factor governs what the minimum difference between the current
		 * upper bound of the graph and the largest y-coordinate should be before the
		 * bound is raised. A larger margin factor implies larger padding */
		final double MARGIN_FACTOR = 0.4;
		
		int ceilingInteger, intervalCount;
		double highestValue = 0, roundedValue, step;
		
		/* Find the highest of the parametrized values and its ceiling (integer) */
		for(double value : values) {
			
			// If the value is higher than the current highest value
			if(value > highestValue) {
				highestValue = value;
			}
		}
		
		ceilingInteger = (int) Math.ceil(highestValue);
		
		// If all values are less than 1 (special case), (5 intervals)
		if(ceilingInteger == 1) {
			
			// Set the starting rounded value to 0.5
			roundedValue = 0.5;
			
			// Set the increment and approximate interval count
			step = 0.5;
			intervalCount = 5;
			
		// The step is probably a multiple of 2 (4 intervals)
		} else if(ceilingInteger <= 8) {
			
			// Round ceilingInteger up to the nearest multiple of 2
			roundedValue = ceilingInteger + (ceilingInteger % 2);
			
			// Set the increment and approximate interval count
			step = 2;
			intervalCount = 4;
						
		// The step is probably a multiple of 5 (5 intervals)
		} else {
			
			// If ceilingInteger is not a multiple of 5, round it up to the nearest one
			if(ceilingInteger%5 != 0) {
				roundedValue = ceilingInteger + (5 - (ceilingInteger % 5));
				
			// Else, set roundedInteger to ceilingInteger
			} else {
				roundedValue = ceilingInteger;
			}
			
			// Set the increment and approximate interval count
			step = 5;
			intervalCount = 5;
		}
		
		// While the difference is not sufficiently large, increment the rounded value
		while((roundedValue - highestValue) <= MARGIN_FACTOR * (roundedValue/intervalCount)) {
			roundedValue += step;
		}
		
		// Set the upper bound
		return roundedValue;
	}
}
