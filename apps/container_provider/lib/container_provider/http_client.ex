defmodule ContainerProvider.HttpClient do
  require Logger

  @spec download_file(uri :: String.t()) :: String.t() | nil
  def download_file(uri) do
    case :httpc.request(:get, {uri, []}, [timeout: 5_000], full_result: false) do
      {:ok, {status, body}} when status < 300 ->
        to_string(body)

      _ ->
        Logger.warn("There was an error while download the file: #{uri}")
        nil
    end
  end
end
