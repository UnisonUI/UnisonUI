defmodule GRPC.Client.GzipCompressor do
  def enabled?, do: Application.get_env(:u_grpc, :compressor_enabled) == true
  def name, do: "gzip"
  def compress(data), do: :zlib.gzip(data)
  def decompress(data), do: :zlib.gunzip(data)
end
