import { FormEvent, useEffect, useState } from "react";

import * as customersApi from "../api/customers";
import * as sitesApi from "../api/sites";

import { apiErrorMessage } from "../api/client";

import type {
  Customer,
  Site,
} from "../types";

export default function SitesPage() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [customerId, setCustomerId] = useState<number>(0);
  const [sites, setSites] = useState<Site[]>([]);

  const [name, setName] = useState("");
  const [address, setAddress] = useState("");

  const [loading, setLoading] = useState(true);
  const [loadingSites, setLoadingSites] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [error, setError] = useState<string | null>(null);

  // =========================================
  // LOAD CUSTOMERS
  // =========================================

  useEffect(() => {
    async function loadCustomers() {
      try {
        setLoading(true);
        setError(null);

        const response =
          await customersApi.listCustomers({
            page: 0,
            size: 100,
          });

        console.log(
          "Customers returned by backend:",
          response.content
        );

        setCustomers(response.content);

        // Automatically select first customer
        if (response.content.length > 0) {
          setCustomerId(response.content[0].id);
        }
      } catch (e) {
        console.error(
          "Failed to load customers:",
          e
        );

        setError(
          apiErrorMessage(
            e,
            "Could not load customers."
          )
        );
      } finally {
        setLoading(false);
      }
    }

    loadCustomers();
  }, []);

  // =========================================
  // LOAD SITES
  // =========================================

  useEffect(() => {
    if (!customerId) {
      setSites([]);
      return;
    }

    async function loadSites() {
      try {
        setLoadingSites(true);
        setError(null);

        console.log(
          "Loading sites for customer:",
          customerId
        );

        const data =
          await sitesApi.listSites(customerId);

        console.log(
          "Sites returned by backend:",
          data
        );

        setSites(data);
      } catch (e) {
        console.error(
          "Failed to load sites:",
          e
        );

        setSites([]);

        setError(
          apiErrorMessage(
            e,
            "Could not load sites."
          )
        );
      } finally {
        setLoadingSites(false);
      }
    }

    loadSites();
  }, [customerId]);

  // =========================================
  // CREATE SITE
  // =========================================

  async function submit(
    e: FormEvent<HTMLFormElement>
  ) {
    e.preventDefault();

    if (!customerId) {
      setError("Please select a customer.");
      return;
    }

    if (!name.trim()) {
      setError("Site name is required.");
      return;
    }

    if (!address.trim()) {
      setError("Site address is required.");
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      await sitesApi.createSite(
        customerId,
        {
          name: name.trim(),
          address: address.trim(),
        }
      );

      setName("");
      setAddress("");

      // Reload sites
      const updatedSites =
        await sitesApi.listSites(customerId);

      setSites(updatedSites);
    } catch (e) {
      console.error(
        "Failed to create site:",
        e
      );

      setError(
        apiErrorMessage(
          e,
          "Could not create site."
        )
      );
    } finally {
      setSubmitting(false);
    }
  }

  // =========================================
  // UI
  // =========================================

  return (
    <div className="page-stack">

      <div className="page-header">
        <h1>Sites</h1>

        <p className="page-header__subtitle">
          Manage locations belonging to customers.
        </p>
      </div>

      {/* CUSTOMER */}

      <section className="panel">

        <h2>Select customer</h2>

        <select
          value={customerId || ""}
          disabled={loading}
          onChange={(e) =>
            setCustomerId(
              Number(e.target.value)
            )
          }
        >
          <option value="">
            {loading
              ? "Loading customers..."
              : "Select customer"}
          </option>

          {customers.map((customer) => (
            <option
              key={customer.id}
              value={customer.id}
            >
              {customer.name}
            </option>
          ))}
        </select>

        {/* CREATE SITE */}

        {customerId > 0 && (
          <form
            className="inline-form"
            onSubmit={submit}
          >
            <input
              required
              placeholder="Site name"
              value={name}
              onChange={(e) =>
                setName(e.target.value)
              }
            />

            <input
              required
              placeholder="Address"
              value={address}
              onChange={(e) =>
                setAddress(e.target.value)
              }
            />

            <button
              type="submit"
              className="btn btn--primary"
              disabled={submitting}
            >
              {submitting
                ? "Adding..."
                : "Add site"}
            </button>
          </form>
        )}

        {error && (
          <div className="form-error">
            {error}
          </div>
        )}
      </section>

      {/* SITES */}

      <section className="panel">

        <h2>Sites</h2>

        {loadingSites ? (
          <p>Loading sites...</p>
        ) : sites.length === 0 ? (
          <p>No sites found.</p>
        ) : (
          <div className="data-table">

            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
              }}
            >

              <thead>
                <tr>

                  <th
                    style={{
                      textAlign: "left",
                      padding: "12px",
                    }}
                  >
                    ID
                  </th>

                  <th
                    style={{
                      textAlign: "left",
                      padding: "12px",
                    }}
                  >
                    Site Name
                  </th>

                  <th
                    style={{
                      textAlign: "left",
                      padding: "12px",
                    }}
                  >
                    Address
                  </th>

                  <th
                    style={{
                      textAlign: "left",
                      padding: "12px",
                    }}
                  >
                    Customer ID
                  </th>

                  <th
                    style={{
                      textAlign: "left",
                      padding: "12px",
                    }}
                  >
                    Created
                  </th>

                </tr>
              </thead>

              <tbody>

                {sites.map((site) => (
                  <tr key={site.id}>

                    <td
                      style={{
                        padding: "12px",
                      }}
                    >
                      {site.id}
                    </td>

                    <td
                      style={{
                        padding: "12px",
                      }}
                    >
                      <strong>
                        {site.name}
                      </strong>
                    </td>

                    <td
                      style={{
                        padding: "12px",
                      }}
                    >
                      {site.address}
                    </td>

                    <td
                      style={{
                        padding: "12px",
                      }}
                    >
                      {site.customerId}
                    </td>

                    <td
                      style={{
                        padding: "12px",
                      }}
                    >
                      {site.createdAt
                        ? new Date(
                            site.createdAt
                          ).toLocaleString()
                        : "-"}
                    </td>

                  </tr>
                ))}

              </tbody>

            </table>

          </div>
        )}

      </section>

    </div>
  );
}