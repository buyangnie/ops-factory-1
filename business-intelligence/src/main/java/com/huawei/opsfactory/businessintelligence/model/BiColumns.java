package com.huawei.opsfactory.businessintelligence.model;

public final class BiColumns {

    // ── Incidents ──
    public static final String ORDER_NUMBER = "ticket_id";
    public static final String PRIORITY = "priority";
    public static final String CATEGORY = "category";
    public static final String ORDER_STATUS = "status";
    public static final String BEGIN_DATE = "opened_at";
    public static final String END_DATE = "closed_at";
    public static final String RESOLVER = "assigned_to";
    public static final String RESPONDER = "assigned_to";
    public static final String RESPONSE_TIME_M = "response_time_minutes";
    public static final String RESOLUTION_TIME_M = "resolution_time_minutes";
    public static final String SLA_COMPLIANT = "SLA Compliant"; // computed injection column
    public static final String CI_AFFECTED = "affected_item";

    // ── Changes ──
    public static final String CHANGE_NUMBER = "ticket_id";
    public static final String CHANGE_TYPE = "change_type";
    public static final String SUCCESS = "close_code";
    public static final String INCIDENT_CAUSED = "incident_ids";
    public static final String REQUESTED_DATE = "opened_at";
    public static final String PLANNED_START = "planned_start_at";

    // ── Requests ──
    public static final String REQUEST_NUMBER = "ticket_id";
    public static final String REQUEST_TYPE = "catalog_item";
    public static final String REQUESTER_DEPT = "requester_dept";
    public static final String REQUEST_RESOLUTION_TIME_M = "resolution_time_minutes";
    public static final String SLA_MET = "SLA Met"; // computed injection column
    public static final String SATISFACTION_SCORE = "satisfaction_score";

    // ── Problems ──
    public static final String PROBLEM_NUMBER = "ticket_id";
    public static final String LOGGED_DATE = "opened_at";
    public static final String ROOT_CAUSE_CATEGORY = "cause_code";
    public static final String KNOWN_ERROR = "known_error";
    public static final String WORKAROUND_AVAILABLE = "workaround";

    // ── Common ──
    public static final String TITLE = "title";
    public static final String STATUS = "status";
    public static final String ASSIGNED_TO = "assigned_to";
    public static final String CLOSED_DATE = "closed_at";
    public static final String RESOLVED_AT = "resolved_at";
    public static final String CLOSE_CODE = "close_code";

    // ── Change extra ──
    public static final String RISK = "risk";
    public static final String ACTUAL_START = "actual_start_at";
    public static final String ACTUAL_END = "actual_end_at";
    public static final String PLANNED_END = "planned_end_at";

    // ── Problem extra ──
    public static final String ROOT_CAUSE = "root_cause";
    public static final String PERMANENT_FIX = "permanent_fix";
    public static final String RELATED_INCIDENT_COUNT = "related_incident_count";

    private BiColumns() {}
}
