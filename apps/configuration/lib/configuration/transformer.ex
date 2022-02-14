defmodule Configuration.Transformer do
  use Toml.Transform
  @levels ["debug", "info", "warn", "error", "all", "none"]
  def transform(:format, "logstash"), do: {LogstashLoggerFormatter, :format}

  def transform(:level, level) when is_binary(level) do
    level = String.downcase(level)
    if level in @levels, do: String.to_atom(level), else: :info
  end

  def transform(:aggregator, storage) when is_binary(storage) do
    String.to_atom(storage)
  end

  def transform(:storage_backend, storage) when is_binary(storage) do
    String.to_atom(storage)
  end

  def transform(_, v), do: v
end
