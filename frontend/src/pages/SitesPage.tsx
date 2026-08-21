import { FormEvent, useEffect, useState } from "react";
import * as customersApi from "../api/customers";
import * as sitesApi from "../api/sites";
import { apiErrorMessage } from "../api/client";
import type { Customer, Site } from "../types";

export default function SitesPage(){
 const [customers,setCustomers]=useState<Customer[]>([]);
 const [customerId,setCustomerId]=useState<number>(0);
 const [sites,setSites]=useState<Site[]>([]);
 const [name,setName]=useState(""); const [address,setAddress]=useState("");
 const [error,setError]=useState<string|null>(null);
 useEffect(()=>{customersApi.listCustomers({page:0,size:100}).then(r=>setCustomers(r.content)).catch(e=>setError(apiErrorMessage(e)));},[]);
 useEffect(()=>{if(customerId) sitesApi.listSites(customerId).then(setSites).catch(e=>setError(apiErrorMessage(e))); else setSites([]);},[customerId]);
 async function submit(e:FormEvent){e.preventDefault(); if(!customerId)return; setError(null);try{await sitesApi.createSite(customerId,{name,address});setName("");setAddress("");setSites(await sitesApi.listSites(customerId));}catch(e){setError(apiErrorMessage(e,"Could not create site."));}}
 return <div className="page-stack"><div className="page-header"><h1>Sites</h1><p className="page-header__subtitle">Manage locations belonging to customers.</p></div>
 <section className="panel"><select value={customerId||""} onChange={e=>setCustomerId(Number(e.target.value))}><option value="">Select customer</option>{customers.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select>
 {customerId&&<form className="inline-form" onSubmit={submit}><input required placeholder="Site name" value={name} onChange={e=>setName(e.target.value)}/><input required placeholder="Address" value={address} onChange={e=>setAddress(e.target.value)}/><button className="btn btn--primary">Add site</button></form>}
 {error&&<div className="form-error">{error}</div>}</section>
 <section className="panel"><h2>Sites</h2>{sites.length===0?<p>No sites found.</p>:<ul>{sites.map(s=><li key={s.id}><strong>{s.name}</strong> — {s.address}</li>)}</ul>}</section></div>
}
