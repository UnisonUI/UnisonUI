defmodule Services do
  @behaviour Services.Behaviour

  @spec available_services :: [Common.Service.t()]
  defdelegate available_services, to: Services.Aggregator

  @spec service(id :: String.t()) :: {:ok, Common.Service.t()} | {:error, :not_found}
  defdelegate service(id), to: Services.Aggregator

  @spec add_source(producer :: GenStage.stage()) ::
          {:ok, GenStage.subscription_tag()}
          | {:error, :bad_args}
          | {:error, :not_a_consumer}
          | {:error, {:bad_opts, String.t()}}
  def add_source(producer) when is_atom(producer) or is_pid(producer),
    do:
      GenStage.sync_subscribe(Services.Aggregator,
        to: producer,
        cancel: :temporary
      )

  def add_source(_producer), do: {:error, :bad_args}
end
