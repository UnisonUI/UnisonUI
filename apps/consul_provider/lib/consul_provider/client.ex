defmodule ConsulProvider.Client do
  import OK, only: [success: 1, failure: 1, ~>>: 2]

  def list_services, do: do_request("/catalog/services") ~>> decode_body

  defp do_request(path),
    do:
      Finch.build(:get, "#{base_url()}#{path}", header(token()))
      |> Finch.request(ConsulProvider.Finch)
      ~>> validate_response
      ~>> Jason.decode()

  defp validate_response(%Finch.Response{status: 200, body: body}), do: success(body)
  defp validate_response(%Finch.Response{body: body}), do: failure(body)
  defp validate_response(error), do: failure(error)

  defp decode_body(services) when is_list(services),
    do:
      Enum.reduce_while(services, success([]), fn service, success(services) ->
        case do_request("/catalog/service/#{service}") do
          success(body) -> {:cont, success(Enum.reduce(body, services, &[&1 | &2]))}
          failure(error) -> {:halt, failure(error)}
        end
      end)

  defp token, do: Application.get_env(:consul_provider, :token, "")
  defp base_url, do: Application.get_env(:consul_provider, :base_url, "")

  defp header(token) when is_bitstring(token) and token != "", do: [{"X-Consul-Token", token}]
  defp header(_), do: []
end
