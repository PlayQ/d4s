package d4s.models.table.index

final case class IndexToUpdate(create: Set[ProvisionedGlobalIndex[?, ?]], modify: Set[GlobalIndexUpdate], delete: Set[String])
