defmodule GitProvider.Git.SupSupervisor do
  use Supervisor

  @spec start_link() :: Supervisor.on_start()
  def start_link, do: Supervisor.start_link(__MODULE__, :ok, name: __MODULE__)

  @impl true
  def init(:ok) do
    Supervisor.init(
      [
        %{
          id: GitProvider.Git.Supervisor,
          start: {GitProvider.Git.Supervisor, :start_link, []}
        },
        {Task,
         fn ->
           GitProvider.Git.Supervisor.start_repositories(
             Application.fetch_env!(:git_provider, :repositories)
           )
         end}
      ],
      strategy: :rest_for_one
    )
  end
end
