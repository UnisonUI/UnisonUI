defmodule Configuration.LoggingTransformer do
  use Toml.Transform
  @levels ["debug", "info", "warn", "error", "all", "none"]
  def transform(:format, "logstash"), do: {LogstashLoggerFormatter, :format}

  def transform(:level, level) when is_binary(level) do
    level = String.downcase(level)
    if level in @levels, do: String.to_atom(level), else: :info
  end

  def transform(_, v), do: v
end
