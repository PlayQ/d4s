package d4s.models.table.index

final case class IndexToUpdate(create: Set[ProvisionedGlobalIndex[_, _]], modify: Set[GlobalIndexUpdate], delete: Set[String])
