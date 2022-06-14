defmodule GitProvider.Github.Connection do
  @behaviour Neuron.Connection
  alias Neuron.{Config, ConfigUtils, JSONParseError, Response}
  @impl true
  def call(body, options) do
    Finch.build(:post, options |> url() |> check_url(), build_headers(options), body)
    |> Finch.request(NeuroFinch)
    |> handle_response(options)
  end

  defp url(options), do: options[:url] || Config.get(:url)

  defp check_url(nil), do: raise(ArgumentError, message: "you need to supply an url")
  defp check_url(url), do: url

  defp build_headers(options),
    do:
      ["Content-Type": "application/json"]
      |> Keyword.merge(headers(options))
      |> Enum.map(fn {key, value} -> {to_string(key), value} end)

  defp headers(options), do: options[:headers] || (Config.get(:headers) || [])

  defp handle_response(response, options) do
    json_library = ConfigUtils.json_library(options)
    parsed_options = ConfigUtils.parse_options(options)
    handle(response, json_library, parsed_options)
  end

  defp handle(response, json_library, parse_options)

  defp handle({:ok, response}, json_library, parse_options) do
    case json_library.decode(response.body, parse_options) do
      {:ok, body} -> build_response_tuple(%{response | body: body})
      {:error, error} -> handle_unparsable(response, error)
      {:error, error, _} -> handle_unparsable(response, error)
    end
  end

  defp handle({:error, _} = response, _, _), do: response

  defp build_response_tuple(%{status: 200} = response) do
    {
      :ok,
      build_response(response)
    }
  end

  defp build_response_tuple(response) do
    {
      :error,
      build_response(response)
    }
  end

  defp build_response(response) do
    %Response{
      status_code: response.status,
      body: response.body,
      headers: response.headers
    }
  end

  defp handle_unparsable(response, error) do
    {
      :error,
      %JSONParseError{
        response: build_response(response),
        error: error
      }
    }
  end
end
