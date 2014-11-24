package co.in.divi.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class Week {
	private static final String		TAG						= Week.class.getSimpleName();

	static SimpleDateFormat			format_date_month_year	= new SimpleDateFormat("d MMM, yyyy");
	static SimpleDateFormat			format_date_month		= new SimpleDateFormat("d MMM");
	static SimpleDateFormat			format_date				= new SimpleDateFormat("d");

	public static SimpleDateFormat	isoFormat				= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

	public long						weekBeginTimestamp;

	Week(long weekBeginTimestamp) {
		this.weekBeginTimestamp = weekBeginTimestamp;
	}

	public static Week getWeek(DiviCalendar cal) {
		DiviCalendar c = new DiviCalendar(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
		c.getTimeInMillis();
		c.set(DiviCalendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
		return new Week(c.getTimeInMillis());
	}

	public Week nextWeek() {
		DiviCalendar c = DiviCalendar.get();
		c.setTimeInMillis(weekBeginTimestamp);
		c.add(DiviCalendar.DATE, 7);
		c.getTimeInMillis();
		return getWeek(c);
	}

	public Date[] getBounds() {
		DiviCalendar startCal = DiviCalendar.get();
		startCal.setTimeInMillis(weekBeginTimestamp);
		startCal.set(DiviCalendar.DAY_OF_WEEK, startCal.getFirstDayOfWeek());
		startCal.getTimeInMillis();

		DiviCalendar endCal = (DiviCalendar) startCal.clone();
		endCal.add(DiviCalendar.DATE, 6);
		endCal.set(DiviCalendar.HOUR_OF_DAY, 23);
		endCal.set(DiviCalendar.MINUTE, 59);
		endCal.set(DiviCalendar.SECOND, 59);
		endCal.set(DiviCalendar.MILLISECOND, 999);
		endCal.getTimeInMillis();

		return new Date[] { startCal.getTime(), endCal.getTime() };
	}

	public String getDisplayString() {
		Date[] bounds = getBounds();
		DiviCalendar now = DiviCalendar.get();
		Week curWeek = Week.getWeek(now);

		if (this.equals(curWeek)) {
			return "This week";
		} else {
			DiviCalendar startCal = DiviCalendar.get();
			startCal.setTimeInMillis(bounds[0].getTime());
			DiviCalendar endCal = DiviCalendar.get();
			endCal.setTimeInMillis(bounds[1].getTime());

			if (startCal.get(DiviCalendar.MONTH) == endCal.get(DiviCalendar.MONTH)) {
				return format_date.format(bounds[0].getTime()) + " - " + format_date_month.format(bounds[1].getTime());
			} else {
				if (startCal.get(DiviCalendar.YEAR) == endCal.get(DiviCalendar.YEAR))
					return format_date_month.format(bounds[0].getTime()) + " - " + format_date_month.format(bounds[1].getTime());
				else
					return format_date_month_year.format(bounds[0].getTime()) + " - " + format_date_month_year.format(bounds[1].getTime());
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Week) {
			return weekBeginTimestamp == ((Week) o).weekBeginTimestamp;
		}
		return false;
	}

	@Override
	public String toString() {
		Date[] bounds = getBounds();
		return "" + bounds[0] + "  to  " + bounds[1];
	}
}
