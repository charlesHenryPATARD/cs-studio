/*
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.utility.ldap;


import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.log4j.Logger;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.model.pvs.ControlSystemEnum;
import org.csstudio.platform.model.pvs.ProcessVariableAdressFactory;
import org.csstudio.platform.util.StringUtil;

/**
 * Constants class for LDAP entries.
 *
 * @author bknerr
 * @version $Revision$
 * @since 11.03.2010
 */
public final class LdapUtils {

    private static final Logger LOG = CentralLogger.getInstance().getLogger(LdapUtils.class.getName());

    /**
     * Returns a filter for 'any' match of the field name (e.g. '<fieldName>=*').
     * @param fieldName the field to match any
     * @return .
     */
    @Nonnull
    public static String any(@Nonnull final String fieldName) {
        return fieldName + LdapFieldsAndAttributes.FIELD_ASSIGNMENT + LdapFieldsAndAttributes.FIELD_WILDCARD;
    }

    /**
     * Returns the attributes for a new entry with the given object class and
     * name.
     *
     * @param keysAndValues an array of Strings that represent key value pairs, consecutively (1st=key, 2nd=value, 3rd=key, 4th=value, etc.)
     * @return the attributes for the new entry.
     */
    @Nonnull
    public static Attributes attributesForLdapEntry(@Nonnull final String... keysAndValues) {
        if (keysAndValues.length % 2 > 0) {
            LOG.error("Ldap Attributes: For key value pairs the length of String array has to be multiple of 2!");
            throw new IllegalArgumentException("Length of parameter keysAndValues has to be multiple of 2.");
        }

        final BasicAttributes result = new BasicAttributes();
        for (int i = 0; i < keysAndValues.length; i+=2) {
            result.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return result;
    }

    /**
     * Assembles and LDAP query from field and value pairs.
     *
     * @param fieldsAndValues an array of Strings that represent key value pairs, consecutively (1st=key, 2nd=value, 3rd=key, 4th=value, etc.)
     * @return the String with <field1>=<value1>, <field2>=<value2> assignements.
     */
    @Nonnull
    public static String createLdapQuery(@Nonnull final String... fieldsAndValues) {
        if (fieldsAndValues.length % 2 > 0) {
            LOG.error("Ldap Attributes: For field and value pairs the length of String array has to be multiple of 2!");
            throw new IllegalArgumentException("Length of parameter fieldsAndValues has to be multiple of 2.");
        }

        final StringBuilder query = new StringBuilder();
        for (int i = 0; i < fieldsAndValues.length; i+=2) {
            query.append(fieldsAndValues[i]).append(LdapFieldsAndAttributes.FIELD_ASSIGNMENT).append(fieldsAndValues[i + 1])
            .append(LdapFieldsAndAttributes.FIELD_SEPARATOR);
        }
        if (query.length() >= 1) {
            query.delete(query.length() - 1, query.length());
        }

        return query.toString();
    }

    /**
     * Filters for forbidden substrings {@link LdapUtils}.
     * @param recordName the name to filter
     * @return true, if the forbidden substring is contained, false otherwise (even for empty and null strings)
     */
    public static boolean filterLDAPNames(@Nonnull final String recordName) {
        if (!StringUtil.hasLength(recordName)) {
            return false;
        }
        for (final String s : LdapFieldsAndAttributes.FORBIDDEN_SUBSTRINGS) {
            if (recordName.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts the given process variable name (<recordName>.<fieldName>) into a
     * record name (<recordName>) which can be
     * looked up in the LDAP directory. If the default control system is EPICS,
     * this will truncate everything after the first dot in the PV name.
     *
     * @param pv
     *            the name of the process variable.
     * @return the name of the record in the LDAP directory.
     */
    @Nonnull
    public static String pvNameToRecordName(@Nonnull final String pv) {
        // TODO (bknerr) : does this epics check really belong here
        if (pv.contains(".") && isEpicsDefaultControlSystem()) {
            return pv.substring(0, pv.indexOf("."));
        }
        return pv;
    }

    /**
     * Returns <code>true</code> if EPICS is the default control system.
     *
     * @return <code>true</code> if EPICS is the default control system,
     *         <code>false</code> otherwise.
     */
    private static boolean isEpicsDefaultControlSystem() {
        final ControlSystemEnum controlSystem =
            ProcessVariableAdressFactory.getInstance().getDefaultControlSystem();
        return controlSystem == ControlSystemEnum.EPICS;
        //              || controlSystem == ControlSystemEnum.DAL_EPICS;
    }

    /**
     * Query result parser for 'name in namespace' row.
     *
     * @author bknerr
     * @author $Author$
     * @version $Revision$
     * @since 09.04.2010
     */
    public static class LdapQueryResult {

        private String _eren;
        private String _econ;
        private String _efan;

        /**
         * Constructor.
         * @param efan .
         * @param econ .
         * @param eren .
         */
        public LdapQueryResult(@Nonnull final String efan,
                               @Nonnull final String econ,
                               @Nonnull final String eren) {
            _efan = efan;
            _econ = econ;
            _eren = eren;
        }

        /**
         * Constructor.
         */
        public LdapQueryResult() {
            // Empty
        }

        /**
         * Getter.
         * @return eren
         */
        @CheckForNull
        public final String getEren() {
            return _eren;
        }

        /**
         * Getter.
         * @return econ
         */
        @CheckForNull
        public final String getEcon() {
            return _econ;
        }

        /**
         * Getter.
         * @return efan
         */
        @CheckForNull
        public final String getEfan() {
            return _efan;
        }

        /**
         * Setter.
         * @param econ .
         */
        public final void setEcon(@Nonnull final String econ) {
            _econ = econ;
        }

        /**
         * Setter.
         * @param efan .
         */
        public final void setEfan(@Nonnull final String efan) {
            _efan = efan;
        }

        /**
         * Setter.
         * @param eren .
         */
        public final void setEren(@Nonnull final String eren) {
            _eren = eren;
        }
    }

    /**
     * Parses a string for fields :
     * 'efan=<valefan>'
     * 'econ=<valecon>'
     * 'eren=<valeren>'
     * and puts the results <valX> if present into the query result.
     *
     * @param ldapPath the line containing a row of a query result
     * @return the query result object
     */
    @Nonnull
    public static LdapQueryResult parseLdapQueryResult(@Nonnull final String ldapPath) {

        final String[] fields = ldapPath.split(LdapFieldsAndAttributes.FIELD_SEPARATOR);

        final LdapQueryResult entry = new LdapQueryResult();

        final String econPrefix = LdapFieldsAndAttributes.ECON_FIELD_NAME + LdapFieldsAndAttributes.FIELD_ASSIGNMENT;
        final String efanPrefix = LdapFieldsAndAttributes.EFAN_FIELD_NAME + LdapFieldsAndAttributes.FIELD_ASSIGNMENT;
        final String erenPrefix = LdapFieldsAndAttributes.EREN_FIELD_NAME + LdapFieldsAndAttributes.FIELD_ASSIGNMENT;

        for (final String field : fields) {
            final String trimmedString = field.trim();
            if (trimmedString.startsWith(econPrefix)) {
                final String econ = trimmedString.substring(econPrefix.length());
                entry.setEcon(econ);
            } else if (trimmedString.startsWith(efanPrefix)){
                final String efan = trimmedString.substring(efanPrefix.length());
                entry.setEfan(efan);
            } else if (trimmedString.startsWith(erenPrefix)) {
                final String eren = trimmedString.substring(erenPrefix.length());
                entry.setEren(eren);
            }
        }
        return entry;
    }

    /**
     * Don't instantiate.
     */
    private LdapUtils() {
        // Empty.
    }

}
