package com.campuslink.eventing;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("eventing")
@EnableScheduling
class EventingSchedulingConfiguration {
}
