defmodule GitProvider.Git.Events.Delete do
  @type t :: %__MODULE__{path: String.t(), repository: GitProvider.Git.Repository.t()}
  defstruct [:path, :repository]

  defimpl Services.Event.From, for: __MODULE__ do
    alias GitProvider.Git.Events.Delete
    alias GitProvider.Git.Repository
    alias Services.Event.Down

    def from(%Delete{path: path, repository: %Repository{name: name, directory: directory}}) do
      path = String.replace_prefix(path, directory, "") |> String.trim_leading("/")
      %Down{id: "#{name}:#{path}"}
    end
  end

  defimpl GitProvider.Git.Events.ContentLoader, for: __MODULE__ do
    def load_content(event), do: {:ok, event}
  end
end
