defmodule Configuration.LoggingTransformer do
  use Toml.Transform
  @levels ["debug", "info", "warn", "error", "all", "none"]
  def transform(:format, "logstash"), do: {LogstashLoggerFormatter, :format}
  def transform(:level, level) when level in @levels, do: String.to_atom(level)
  def transform(_, v), do: v
end
