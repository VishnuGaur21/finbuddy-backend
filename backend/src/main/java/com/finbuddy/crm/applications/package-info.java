/**
 * Applications module: Loan origination system (LOS), partner dispatches, offers, and disbursement tracking.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"shared", "leads", "partners"}
)
package com.finbuddy.crm.applications;
