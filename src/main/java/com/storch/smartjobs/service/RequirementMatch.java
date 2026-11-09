package com.storch.smartjobs.service;

public record RequirementMatch(String requirement, Status status) {
    public enum Status { MET, PARTIAL, MISSING }
}
