package co.in.divi.model;

import co.in.divi.db.model.Attempt;
import co.in.divi.db.model.Command;

public class SyncDownResponse {

	public Attempt[]	attempts;

	public Command[]	commands;

	public boolean		hasMoreData;
}