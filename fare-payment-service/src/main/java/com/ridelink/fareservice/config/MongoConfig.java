package com.ridelink.fareservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB Auditing configuration.
 * Separated from main application class to allow clean @WebMvcTest slices without requiring Mongo beans.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
