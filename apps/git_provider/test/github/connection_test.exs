defmodule GitProvider.Github.ConnectionTest do
  use ExUnit.Case
  alias GitProvider.Github.{Connection}
  import Mock

  describe "call/2" do
    test "successful call" do
      response = %{"data" => "test"}

      with_mock Finch, [:passthrough],
        request: fn _, _ ->
          {:ok,
           %Finch.Response{
             status: 200,
             body: Jason.encode!(response)
           }}
        end do
        assert Connection.call("{}",
                 url: "http://localhost",
                 headers: [Authorization: "bearer token"]
               ) ==
                 {:ok, %Neuron.Response{body: response, status_code: 200, headers: []}}
      end
    end

    test "failed request" do
      response = %{"data" => "test"}

      with_mock Finch, [:passthrough],
        request: fn _, _ ->
          {:ok,
           %Finch.Response{
             status: 404,
             body: Jason.encode!(response)
           }}
        end do
        assert Connection.call("{}",
                 url: "http://localhost",
                 headers: [Authorization: "bearer token"]
               ) ==
                 {:error, %Neuron.Response{body: response, status_code: 404, headers: []}}
      end
    end

    test "failed to decode json" do
      with_mock Finch, [:passthrough],
        request: fn _, _ ->
          {:ok,
           %Finch.Response{
             status: 200,
             body: "Not a json"
           }}
        end do
        assert Connection.call("{}",
                 url: "http://localhost",
                 headers: [Authorization: "bearer token"]
               ) ==
                 {:error,
                  %Neuron.JSONParseError{
                    error: %Jason.DecodeError{data: "Not a json", position: 0},
                    response: %Neuron.Response{body: "Not a json", status_code: 200, headers: []}
                  }}
      end
    end
  end
end
