defmodule Services.Behaviour do
  @callback available_services :: [Common.Service.t()]
  @callback service(id :: String.t()) :: {:ok, Common.Service.t()} | {:error, :not_found}

  @callback add_source(producer :: GenStage.stage()) ::
          {:ok, GenStage.subscription_tag()}
          | {:error, :bad_args}
          | {:error, :not_a_consumer}
          | {:error, {:bad_opts, String.t()}}
end
