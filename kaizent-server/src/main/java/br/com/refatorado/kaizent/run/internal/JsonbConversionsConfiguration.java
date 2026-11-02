package br.com.refatorado.kaizent.run.internal;

import br.com.refatorado.kaizent.intake.ProblemAttributes;
import br.com.refatorado.kaizent.resolution.Proposal;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import tools.jackson.databind.json.JsonMapper;

/**
 * Maps the value types stored in {@code jsonb} columns (ADR-0023). Spring Data JDBC has a single
 * set of custom conversions, so the types of every module are registered here, keeping the
 * dialect's own conversions.
 */
@Configuration(proxyBeanMethods = false)
class JsonbConversionsConfiguration {

  /** Value types stored as {@code jsonb}. */
  static final List<Class<?>> JSONB_TYPES = List.of(ProblemAttributes.class, Proposal.class);

  @Bean
  JdbcCustomConversions kaizentJdbcCustomConversions(JdbcDialect dialect, JsonMapper json) {
    return JdbcCustomConversions.of(
        dialect,
        JSONB_TYPES.stream()
            .<Object>mapMulti((type, sink) -> {
              sink.accept(new ToJsonb(json, type));
              sink.accept(new FromJsonb(json, type));
            })
            .toList());
  }

  @WritingConverter
  record ToJsonb(JsonMapper json, Class<?> type) implements GenericConverter {

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Set.of(new ConvertiblePair(type, PGobject.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
        return null;
      }
      PGobject value = new PGobject();
      value.setType("jsonb");
      try {
        value.setValue(json.writeValueAsString(source));
      } catch (SQLException e) {
        throw new IllegalStateException(e);
      }
      return value;
    }
  }

  @ReadingConverter
  record FromJsonb(JsonMapper json, Class<?> type) implements GenericConverter {

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Set.of(new ConvertiblePair(PGobject.class, type));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (!(source instanceof PGobject value) || value.getValue() == null) {
        return null;
      }
      return json.readValue(value.getValue(), type);
    }
  }
}
