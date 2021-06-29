defmodule ContainerProvider.HttpClient do
  require Logger

  @spec download_file(uri :: String.t()) :: String.t() | nil
  def download_file(uri) do
    response = Finch.build(:get, uri) |> Finch.request(FinchHttpClient)

    case response do
      {:ok, %Finch.Response{status: 200, body: body}} ->
        body

      {:error, error} ->
        Logger.warn("There was an error while download the file: #{Exception.message(error)}")
        nil
    end
  end
end
