/*
 * Copyright 2010-11 Brookhaven National Laboratory
 * All rights reserved. Use is subject to license terms.
 */
package org.epics.pvmanager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation class for {@link PVWriter}.
 *
 * @author carcassi
 */
class PVWriterImpl<T> implements PVWriter<T> {
    
    static <T> PVWriterImpl<T> implOf(PVWriter<T> pvWriter) {
        if (pvWriter instanceof PVWriterImpl) {
            return (PVWriterImpl<T>) pvWriter;
        }
        
        throw new IllegalArgumentException("PVWriter must be implemented using PVWriterImpl");
    }

    private List<PVWriterListener> pvWriterListeners = new CopyOnWriteArrayList<PVWriterListener>();
    private AtomicReference<Exception> lastWriteException = new AtomicReference<Exception>();
    private volatile  WriteDirector<T> writeDirector;
    private final boolean syncWrite;
    private final boolean notifyFirstListener; 

    PVWriterImpl(boolean syncWrite, boolean notifyFirstListener) {
        this.syncWrite = syncWrite;
        this.notifyFirstListener = notifyFirstListener;
    }
    
    void firePvWritten() {
        for (PVWriterListener listener : pvWriterListeners) {
            listener.pvWritten();
        }
    }

    void setWriteDirector(WriteDirector<T> writeDirector) {
        this.writeDirector = writeDirector;
        writeDirector.init();
    }

    /**
     * Adds a listener to the value. This method is thread safe.
     *
     * @param listener a new listener
     */
    @Override
    public void addPVWriterListener(PVWriterListener listener) {
        if (isClosed())
            throw new IllegalStateException("Can't add listeners to a closed PV");
        
        // Check whether to notify when the first listener is added.
        // This is done to make sure that exceptions thrown at pv creation
        // are not lost since the listener is added after the pv is created.
        // If the notification is done on a separate thread, the context switch
        // is enough to make sure the listener is registerred before the event
        // arrives, but if the notification is done on the same thread
        // the notification would be lost.
        boolean notify = pvWriterListeners.isEmpty() && notifyFirstListener &&
                lastWriteException.get() != null;
        pvWriterListeners.add(listener);
        if (notify)
            listener.pvWritten();
    }

    /**
     * Removes a listener to the value. This method is thread safe.
     *
     * @param listener the old listener
     */
    @Override
    public void removePVWriterListener(PVWriterListener listener) {
        pvWriterListeners.remove(listener);
    }
    
    
    @Override
    public void write(T newValue) {
        if (syncWrite) {
            writeDirector.syncWrite(newValue, this);
        } else {
            writeDirector.write(newValue, this);
        }
    }

    // Theoretically, this should be checked only on the client thread.
    // Better to be conservative, and guarantee that another thread
    // cannot add a listener when another is closing.
    private AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * De-registers all listeners, stops all notifications and closes all
     * connections from the data sources needed by this. Once the PV
     * is closed, it can't be re-opened. Subsequent calls to close do not
     * do anything.
     */
    @Override
    public void close() {
        boolean wasClosed = closed.getAndSet(true);
        if (!wasClosed) {
            pvWriterListeners.clear();
            writeDirector.close();
        }
    }

    /**
     * True if no more notifications are going to be sent for this PV.
     *
     * @return true if closed
     */
    @Override
    public boolean isClosed() {
        return closed.get();
    }
    
    /**
     * Changes the last exception associated with write operations.
     * 
     * @param ex the new exception
     */
    void setLastWriteException(Exception ex) {
        lastWriteException.set(ex);
    }

    /**
     * Returns the last exception that was generated by write operations
     * and clears it (subsequent call will return null).
     *
     * @return the last generated exception or null
     */
    @Override
    public Exception lastWriteException() {
        return lastWriteException.getAndSet(null);
    }
    
}
