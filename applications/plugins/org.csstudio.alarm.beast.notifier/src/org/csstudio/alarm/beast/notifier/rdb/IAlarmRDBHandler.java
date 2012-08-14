package org.csstudio.alarm.beast.notifier.rdb;

import org.csstudio.alarm.beast.TreeItem;
import org.csstudio.alarm.beast.client.AlarmTreeItem;
import org.csstudio.alarm.beast.client.AlarmTreeRoot;
import org.csstudio.alarm.beast.notifier.AlarmNotifier;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelListener;

/**
 * Interface for alarm model wrapper.
 * @author Fred Arnaud (Sopra Group)
 *
 */
public interface IAlarmRDBHandler extends AlarmClientModelListener {
	
	/** Initialize handler with {@link AlarmNotifier} */
	public void init(final AlarmNotifier notifier);
	
	/** Find item by path */
	public AlarmTreeItem findItem(String path);
	
	/** Get current {@link AlarmTreeRoot} */
	public TreeItem getAlarmTree();
	
	/** Release model */
	public void close();
}
