package org.csstudio.archive.diirt.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.csstudio.archive.reader.ArchiveReader;
import org.csstudio.archive.reader.ArchiveRepository;
import org.csstudio.archive.reader.ValueIterator;
import org.diirt.datasource.MultiplexedChannelHandler;
import org.diirt.datasource.ValueCache;
import org.diirt.util.time.Timestamp;
import org.diirt.vtype.Time;
import org.diirt.vtype.VType;

/**
 *
 * <code>AbstractChannelHandler</code> is the base channel handler for the archive data source. It provides common
 * methods necessary to fetch data by the static and dynamic channel handlers.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public abstract class AbstractChannelHandler extends MultiplexedChannelHandler<Boolean, List<VType>> {

    protected static final Logger LOGGER = Logger.getLogger(AbstractChannelHandler.class.getName());

    private final String strippedName;
    private final ArchiveSource[] sources;
    private final int binCount;

    /**
     * Construct a new channel handler.
     *
     * @param fullChannelName the full unique channel name
     * @param strippedChannelName the stripped channel name (the pv name that archiver understands)
     * @param binCount number of bins for optimised data retrieval
     * @param sources the sources to use to fetch the data
     */
    protected AbstractChannelHandler(String fullChannelName, String strippedChannelName, int binCount,
        ArchiveSource... sources) {
        super(fullChannelName);
        this.strippedName = strippedChannelName;
        this.sources = sources;
        this.binCount = binCount;
    }

    /**
     * Loads values for the the given time window. The values are stored by timestamp.
     *
     * @param startTime the start of the time window
     * @param endTime the end of the time window
     * @param optimised true for optimised retrieval, false for raw values
     * @return the list of all found values
     * @throws Exception if there was an error fetching the data
     */
    private List<VType> loadValuesForTimeWindow(Timestamp startTime, Timestamp endTime, boolean optimised)
        throws Exception {
        List<VType> values = new ArrayList<>(5000);
        for (ArchiveSource as : sources) {
            ArchiveReader archive = ArchiveRepository.getInstance().getArchiveReader(as.url);
            ValueIterator iterator = optimised
                ? archive.getOptimizedValues(as.key, strippedName, startTime, endTime, binCount)
                : archive.getRawValues(as.key, strippedName, startTime, endTime);
            VType temp;
            while (iterator.hasNext()) {
                temp = iterator.next();
                if (temp instanceof Time) {
                    values.add(temp);
                }
            }
        }
        Collections.sort(values, new Comparator<VType>() {
            @Override
            public int compare(VType o1, VType o2) {
                return ((Time) o1).getTimestamp().compareTo(((Time) o2).getTimestamp());
            }
        });
        return values;
    }

    /**
     * Loads a single value that matches the given timestamp.
     *
     * @param time the time for which the value should be loaded
     * @return the value if found or nothing if not found
     * @throws Exception if there is an exception while fetching data from the archive
     */
    private Optional<VType> loadValueForTime(Timestamp time) throws Exception {
        VType theValue = null;
        Timestamp theTimestamp = null;
        for (ArchiveSource as : sources) {
            ArchiveReader archive = ArchiveRepository.getInstance().getArchiveReader(as.url);
            ValueIterator iterator = archive.getRawValues(as.key, strippedName, time, time);
            VType temp = null;
            VType value = null;
            Timestamp t = null;
            while (iterator.hasNext()) {
                // find the first value that has a timestamp greater than start
                temp = iterator.next();
                if (((Time) temp).getTimestamp().compareTo(time) > 0) {
                    break;
                }
                value = temp;
                t = ((Time) temp).getTimestamp();
            }
            // value is the last value with a timestamp smaller than the requested time
            if (value != null) {
                // if a value like that was found assign it
                if (theTimestamp == null) {
                    theValue = value;
                    theTimestamp = t;
                } else if (theTimestamp.compareTo(t) > 0) {
                    // but only if non was found yet, or if this is the value that is the closest to the requested time
                    theValue = value;
                    theTimestamp = t;
                }
            }
        }
        return Optional.ofNullable(theValue);
    }

    /**
     * Fetch data from the archiver.
     *
     * @param startTime the start time of the fetch interval
     * @param endTime the end time of the fetch interval
     * @param optimised true for optimised retrieval, false for raw values
     * @throws Exception in case of an error fetching the data from the archive reader
     */
    protected void fetchData(Timestamp startTime, Timestamp endTime, boolean optimised)
        throws Exception {
        List<VType> values;
        boolean singleValue = startTime.equals(endTime);
        if (singleValue) {
            values = new ArrayList<>();
            loadValueForTime(startTime).ifPresent(e -> values.add(e));
        } else {
            values = loadValuesForTimeWindow(startTime, endTime, optimised);
        }
        processMessage(values);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.diirt.datasource.MultiplexedChannelHandler#disconnect()
     */
    @Override
    protected void disconnect() {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     *
     * @see org.diirt.datasource.MultiplexedChannelHandler#isConnected(java.lang.Object)
     */
    @Override
    protected boolean isConnected(Boolean payload) {
        return payload;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.diirt.datasource.MultiplexedChannelHandler#findTypeAdapter(org.diirt.datasource.ValueCache,
     * java.lang.Object)
     */
    @Override
    protected ArchiveTypeAdapter findTypeAdapter(ValueCache<?> cache, Boolean connPayload) {
        return new ArchiveTypeAdapter();
    }
}
