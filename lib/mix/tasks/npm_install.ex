defmodule Mix.Tasks.Npm.Install do
  use Mix.Task

  def run(_) do
       webapp = ["apps", "unison_ui", "webapp"] |> Path.join()|> Path.expand()
    System.cmd("npm", ["install"], cd: webapp)
  end
end
