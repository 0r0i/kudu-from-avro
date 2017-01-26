# Kudu from Avro and SQL

This tool can create a Kudu table from an Avro schema or from a (Impala) SQL script. (the SQL part came later :-))

# Usage

```
Usage: kudu-from-avro [options]

  -t, --table <value>      Table to create in Kudu
  -p, --primary_key <value>
                           Primary key column name in the Kudu table
  -r, --replica <value>    Number of replicas (default: 3)
  -b, --buckets <value>    Number of buckets (default: 32)
  -s, --avro_schema <value>
                           .avsc to read to create the table
  -c, --compressed <value>
                           Compress columns using LZ4
  -k, --kudu_servers <value>
                           Kudu master tablets
  -q, --sql <value>        Custom SQL creation to create columns from: "id INTEGER, ts BIGINT, name STRING"
```

## Compound keys

`-p` supports a compound primary key `-p id,company_id`; those columns will be the first in the Kudu table, as required by Kudu.

# Create a Kudu table from an Avro schema
 
```
$ ./kudu-from-avro -t my_new_table -p id -s schema.avsc -k kudumaster01
```

# Create a Kudu table from a SQL script

Note that it defaults all columns to _nullable_ (except the keys of course).

```
$ ./kudu-from-avro -q "id STRING, ts BIGINT, name STRING" -t my_new_table -p id -k kudumaster01
```

# How to build it

```
$ sbt universal:packageBin
```

The `.zip` will be available in `target/universal/kudu-from-avro-1.0.zip`, and the executable inside: `bin/kudu-from-avro`.

