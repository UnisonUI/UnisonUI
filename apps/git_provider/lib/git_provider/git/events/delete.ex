defmodule GitProvider.Git.Events.Delete do
  @type t :: %__MODULE__{path: String.t(), repository: GitProvider.Git.Repository.t()}
  defstruct [:path, :repository]

  defimpl Services.Event.From, for: GitProvider.Git.Events.Delete do
    alias GitProvider.Git.Events.Delete
    alias GitProvider.Git.Repository
    alias Services.Event

    def from(%Delete{path: path, repository: %Repository{name: name, directory: directory}}) do
      path = String.replace_prefix(path, directory, "") |> String.trim_leading("/")
      %Event.Down{id: "#{name}:#{path}"}
    end
  end

  defimpl GitProvider.Git.Events.ContentLoader, for: GitProvider.Git.Events.Delete do
    def load_content(event), do: {:ok, event}
  end
end
