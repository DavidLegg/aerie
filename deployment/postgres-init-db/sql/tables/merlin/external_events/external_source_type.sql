create table merlin.external_source_type (
    name text not null,

    constraint external_source_type_pkey
      primary key (name)
);

comment on table merlin.external_source_type is e''
  'Externally imported event source types (each external source has to be of a certain type).\n'
  'They are also helpful to classify external sources.\n'
  'Derivation groups are a subclass of external source type.';

comment on column merlin.external_source_type.name is e''
  'The identifier for this external_source_type, as well as its name.';
