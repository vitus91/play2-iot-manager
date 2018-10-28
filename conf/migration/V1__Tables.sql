
create table "owner" (
  "ID" bigserial PRIMARY KEY NOT NULL,
  "company" VARCHAR(255),
  "first_name" VARCHAR(255),
  "last_name" VARCHAR(255),
  "zip" INTEGER,
  "city" VARCHAR(255),
  "street" VARCHAR(255),
  "street_2" VARCHAR(255),
  "created_date" TIMESTAMP,
  "changed_date" TIMESTAMP,
  "deleted" boolean default false not NULL
);

create table "device" (
  "ID" bigserial PRIMARY KEY NOT NULL,
  "ownerID" BIGINT REFERENCES owner,
  "name" VARCHAR(255),
  "device" VARCHAR(255),
  "device_serial_number" VARCHAR(255),
  "location_lat" double precision,
  "location_lon" double precision,
  "created_date" TIMESTAMP,
  "changed_date" TIMESTAMP,
  "deleted" boolean default false not NULL
)