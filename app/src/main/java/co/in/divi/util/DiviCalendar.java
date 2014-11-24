package co.in.divi.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

/*
 * Just a GregorianCalendar with fixed (locale independent) week numbering scheme.
 */
public class DiviCalendar extends GregorianCalendar {

	public static final int	FIRST_DAY_OF_WEEK			= Calendar.MONDAY;
	public static final int	MINIMAL_DAYS_IN_FIRST_WEEK	= 4;

	public static DiviCalendar get() {
		DiviCalendar cal = new DiviCalendar();
		cal.setFirstDayOfWeek(FIRST_DAY_OF_WEEK);
		cal.setMinimalDaysInFirstWeek(MINIMAL_DAYS_IN_FIRST_WEEK);
		cal.getTimeInMillis();
		return cal;
	}

	private DiviCalendar() {
	}

	public DiviCalendar(int year, int month, int date) {
		super(year, month, date);
		setFirstDayOfWeek(FIRST_DAY_OF_WEEK);
		setMinimalDaysInFirstWeek(MINIMAL_DAYS_IN_FIRST_WEEK);
		getTimeInMillis();
	}
}
