/**
 * Finance module: Commercial payout slabs, commission ledgers, GST invoicing, and partner settlements.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"shared", "partners", "applications"}
)
package com.finbuddy.crm.finance;
