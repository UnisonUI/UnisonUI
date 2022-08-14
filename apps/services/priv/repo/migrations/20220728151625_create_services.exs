defmodule Services.Storage.Sql.Postgres.Repo.Migrations.CreateServices do
  use Ecto.Migration

  def change do
    create table(:services) do
      add :first_name, :string
      add :last_name, :string
      add :age, :integer
    end
  end
end
