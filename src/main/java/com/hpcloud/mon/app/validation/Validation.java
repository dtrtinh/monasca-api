package com.hpcloud.mon.app.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.hpcloud.mon.common.model.Services;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Validation related utilities.
 * 
 * @author Jonathan Halterman
 */
public final class Validation {
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
  private static final Splitter COLON_SPLITTER = Splitter.on(':').omitEmptyStrings().trimResults();
  private static final DateTimeFormatter ISO_8601_FORMATTER = ISODateTimeFormat.dateTimeParser();
  private static final List<String> VALID_STATISTICS = Arrays.asList("avg", "min", "max", "sum",
      "count");

  private Validation() {
  }

  /**
   * @throws WebApplicationException if the {@code date} invalid or is required and null.
   */
  public static DateTime parseAndValidateDate(String date, String parameterName, boolean required) {
    if (Strings.isNullOrEmpty(date)) {
      if (required)
        throw Exceptions.unprocessableEntity("%s is required", parameterName);
      else
        return null;
    }

    try {
      return ISO_8601_FORMATTER.parseDateTime(date);
    } catch (Exception e) {
      throw Exceptions.unprocessableEntity("%s must be an ISO 8601 formatted time", parameterName);
    }
  }

  /**
   * @throws WebApplicationException if the {@code value} is null or empty.
   */
  public static Map<String, String> parseAndValidateNameAndDimensions(String name,
      String dimensionsStr) {
    Validation.validateNotNullOrEmpty(dimensionsStr, "dimensions");

    Map<String, String> dimensions = new HashMap<String, String>();
    for (String dimensionStr : COMMA_SPLITTER.split(dimensionsStr)) {
      String[] dimensionArr = Iterables.toArray(COLON_SPLITTER.split(dimensionStr), String.class);
      if (dimensionArr.length == 2)
        dimensions.put(dimensionArr[0], dimensionArr[1]);
    }

    String service = dimensions.get(Services.SERVICE_DIMENSION);
    MetricNameValidation.validate(name, service);
    DimensionValidation.validate(dimensions, service);
    return dimensions;
  }

  /**
   * @throws WebApplicationException if the {@code number} is invalid.
   */
  public static int parseAndValidateNumber(String number, String parameterName) {
    try {
      return Integer.parseInt(number);
    } catch (NumberFormatException e) {
      throw Exceptions.unprocessableEntity("%s must be valid number", parameterName);
    }
  }

  /**
   * @throws WebApplicationException if the {@code statistics} empty or invalid.
   */
  public static List<String> parseValidateAndNormalizeStatistics(Iterable<String> statistics) {
    List<String> validStats = new ArrayList<String>(5);
    for (String statistic : statistics) {
      String statisticLower = statistic.toLowerCase();
      if (!VALID_STATISTICS.contains(statistic))
        throw Exceptions.unprocessableEntity("%s is not a valid statistic", statistic);
      validStats.add(statisticLower);
    }

    if (validStats.isEmpty())
      throw Exceptions.unprocessableEntity("Statistics are required");

    return validStats;
  }

  /**
   * @throws WebApplicationException if the {@code value} is null or empty.
   */
  public static void validateNotNullOrEmpty(String value, String parameterName) {
    if (Strings.isNullOrEmpty(value))
      throw Exceptions.unprocessableEntity("%s is required", parameterName);
  }

  /**
   * @throws WebApplicationException if the {@code startTime} or {@code endTime} are invalid
   */
  public static void validateTimes(DateTime startTime, DateTime endTime) {
    if (!startTime.isBefore(endTime))
      throw Exceptions.badRequest("start_time must be before end_time");
  }
}