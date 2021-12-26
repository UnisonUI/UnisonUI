defmodule UnisonUI.Application do
  @moduledoc false
  use Application

  def start(_type, _args) do
    children = [
      UnisonUI.SelfSpecificationServer,
      {Task.Supervisor, name: UnisonUI.TaskSupervisor},
      Plug.Cowboy.child_spec(
        scheme: :http,
        plug: UnisonUI.Routes,
        options: [
          port: Application.get_env(:unison_ui, :port, 8080),
          dispatch: dispatch()
        ]
      ),
      %{id: UnisonUI.Services.Supervisor, start: {UnisonUI.Services.Supervisor, :start_link, []}}
    ]

    opts = [strategy: :one_for_one, name: UnisonUI.Supervisor]
    Supervisor.start_link(children, opts)
  end

  defp dispatch do
    [
      {:_,
       [
         {"/ws", UnisonUI.Routes.Realtime, []},
         {:_, Plug.Cowboy.Handler, {UnisonUI.Routes, []}}
       ]}
    ]
  end
end
