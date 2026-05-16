package com.healthagent.agent;

import lombok.Data;

@Data
public class Observation {
    private String content;

    public Observation() {}

    public Observation(String content) {
        this.content = content;
    }
}
